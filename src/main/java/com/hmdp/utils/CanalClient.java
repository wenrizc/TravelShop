package com.hmdp.utils;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class CanalClient implements DisposableBean {

    @Value("${canal.host:localhost}")
    private String canalHost;

    @Value("${canal.port:11111}")
    private int canalPort;

    @Value("${canal.destination:example}")
    private String destination;

    @Value("${canal.username:}")
    private String username;

    @Value("${canal.password:}")
    private String password;

    private final RabbitTemplate rabbitTemplate;
    private CanalConnector connector;
    private ExecutorService executorService;
    private volatile boolean running = false;

    private static final String EXCHANGE_NAME = "db.changes.exchange";
    private static final String ROUTING_KEY = "db.change";

    @Autowired
    public CanalClient(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void init() {
        // 创建canal连接
        connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(canalHost, canalPort),
                destination,
                username,
                password
        );

        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("canal-client-thread");
            thread.setDaemon(true);
            return thread;
        });

        start();
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;
        executorService.submit(() -> {
            try {
                connector.connect();
                connector.subscribe(".*\\.tb_shop,.*\\.tb_blog,.*\\.tb_voucher");
                connector.rollback();

                log.info("Canal客户端启动成功，开始监听数据库变更...");

                while (running) {
                    // 获取数据
                    Message message = connector.getWithoutAck(100);
                    long batchId = message.getId();

                    try {
                        List<CanalEntry.Entry> entries = message.getEntries();
                        if (entries != null && !entries.isEmpty()) {
                            for (CanalEntry.Entry entry : entries) {
                                if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                                    publishRowData(entry);
                                }
                            }
                        }
                        connector.ack(batchId);
                    } catch (Exception e) {
                        log.error("处理Canal消息异常", e);
                        connector.rollback(batchId);
                    }

                    // 防止空转
                    if (message.getEntries().isEmpty()) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Canal客户端异常", e);
            } finally {
                stop();
            }
        });
    }

    private void publishRowData(CanalEntry.Entry entry) throws Exception {
        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
        String tableName = entry.getHeader().getTableName();
        CanalEntry.EventType eventType = rowChange.getEventType();

        for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
            Map<String, Object> data = new HashMap<>();

            if (eventType == CanalEntry.EventType.DELETE) {
                extractColumns(data, rowData.getBeforeColumnsList());
            } else {
                extractColumns(data, rowData.getAfterColumnsList());
            }

            String operation;
            switch (eventType) {
                case INSERT:
                    operation = "INSERT";
                    break;
                case UPDATE:
                    operation = "UPDATE";
                    break;
                case DELETE:
                    operation = "DELETE";
                    break;
                default:
                    continue;
            }

            Map<String, Object> message = new HashMap<>();
            message.put("table", tableName);
            message.put("operation", operation);
            message.put("data", data);

            // 发送到RabbitMQ
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, message);
            log.info("发送数据库变更消息到MQ: table={}, operation={}, id={}",
                    tableName, operation, data.get("id"));
        }
    }

    private void extractColumns(Map<String, Object> data, List<CanalEntry.Column> columns) {
        for (CanalEntry.Column column : columns) {
            data.put(column.getName(), column.getValue());
        }
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        if (connector != null) {
            connector.disconnect();
        }
        log.info("Canal客户端已停止");
    }

    @Override
    public void destroy() {
        stop();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}