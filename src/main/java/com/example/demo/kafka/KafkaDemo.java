package com.example.demo.kafka;

import com.alibaba.fastjson2.JSON;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class KafkaDemo {

    public static void myConsumer(String bootstrap, String user, String passwd, String groupId, String topic) {
        System.out.println("--------kafka consumer demo---------");

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        // true 自动提交，由api自动管理消息消费后的提交策略
        // false 手动提交，在消息消费后手动调用commitAsync()进行提交
        // 如果能容忍客户端异常中断恢复时消息的重复消费，建议使用自动提交来简化开发；如果对数据幂等性有严格要求，建议手动提交
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // 示例使用手动提交
        // earliest 当各分区下有已提交的offset时，从提交的offset开始消费；无提交的offset时，从最早的数据开始消费
        // latest 当各分区下有已提交的offset时，从提交的offset开始消费；无提交的offset时，从最新的数据开始消费
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // 建议使用earliest
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100); // 每次调用poll()所能获取的最大记录数，建议根据消费者的处理能力调整该值
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // 认证安全协议和认证信息
//        props.put("security.protocol", "SASL_PLAINTEXT");
//        props.put("sasl.mechanism", "SCRAM-SHA-512");
//        props.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username='"
//                + user + "' password='" + passwd + "';");

        KafkaConsumer consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topic));


        // 通过循环不断从kafka抽取数据
        // 生产环境请单独开辟线程做数据抽取
        while (true) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100)); // 轮询最大间隔时间
                if (records.isEmpty()) {
                    continue;
                }
                for (ConsumerRecord<String, String> record : records) {
                    // 在此处理每个消息
                    System.out.println("Partition: " + record.partition() + " Offset: " + record.offset() + " Value: " + record.value());
                    SendDemo sendDemo = JSON.parseObject(record.value(), SendDemo.class);
                    System.out.println("Device: " + sendDemo.getName());
                    // 。。。
                }

                // 每次拉取后异步提交
                // 生产环境请确保在所有已拉取消息处理成功后再提交
                consumer.commitAsync();
            } catch (Exception e) {
                System.out.println("kafka consumer error: " + e);
            }
        }
    }

    public static void myProducer(String bootstrap, String user, String passwd, String topic) {
        System.out.println("--------kafka producer demo---------");

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 100); // 批处理等待时间，建议100ms
        // 批处理大小。默认大小16KB，如果单条消息普遍大于16KB，建议设置为单条消息平均大小的倍数来启用批处理
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 示例使用默认值
        // 调用send()方法后等待响应的最大时间，建议不小于30秒
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000); // 示例使用默认值2min
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 认证安全协议和认证信息
//        props.put("security.protocol", "SASL_PLAINTEXT");
//        props.put("sasl.mechanism", "SCRAM-SHA-512");
//        props.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username='"
//                + user + "' password='" + passwd + "';");

        // 创建生产者实例
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        try {
            // 模拟10个设备发送消息
            int i = 1;
            while (true) {
                // 为了保证消费者能顺序消费同一个设备的消息，key的值需要填写设备唯一编码，在生产中建议使用mac地址或imei等
                // 示例使用1到10作为设备编号
                String key = String.format("%d", (int) (Math.random() * 10) + 1);
                // 消息内容
                SendDemo sendDemo = new SendDemo();
                sendDemo.setId(i);
                sendDemo.setName("Device-" + key);
                sendDemo.setEmail("Device-" + key + "@example.com");
                sendDemo.setPhone("+86-" + i);
                //String value = "Message-" + i + " at " + System.currentTimeMillis();
                String value = JSON.toJSONString(sendDemo);

                ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

                producer.send(record, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception exception) {
                        if (exception == null) {
                            System.out.printf("Send succeed - Topic: %s, Partition: %d, Offset: %d%n",
                                    metadata.topic(), metadata.partition(), metadata.offset());
                        } else {
                            System.err.println("Send failed: " + exception.getMessage());
                        }
                    }
                });

                i++;
                Thread.sleep(1000); // 模拟延迟
            }

        } catch (Exception e) {
            System.out.println("kafka producer error: " + e);
        }
    }

    public static void main(String[] args) {
        String bootstrap = "192.168.1.53:9092";
        String user = "test";
        String passwd = "12345";
        String groupId = "Combined Lag-1";

        List<String> topics = new ArrayList<>();
        topics.add("xa-asoms-001");
        topics.add("xa-asoms-002");
        topics.add("xa-asoms-realair");
        topics.add("xa-hpps-001");
        topics.add("xa-hpps-002");
        topics.add("xa-hpps-003");

        myProducerMultipleTopics(bootstrap, user, passwd, topics);
    }

    public static void myProducerMultipleTopics(String bootstrap, String user, String passwd, List<String> topics) {
        System.out.println("--------kafka producer multiple topics demo---------");

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 100);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        try {
            int messageIndex = 1;
            
            while (true) {
                for (String topic : topics) {
                    String key = String.format("%d", (int) (Math.random() * 10) + 1);
                    
                    SendDemo sendDemo = new SendDemo();
                    sendDemo.setId(messageIndex);
                    sendDemo.setName("Device-" + key);
                    sendDemo.setEmail("Device-" + key + "@example.com");
                    sendDemo.setPhone("+86-" + messageIndex);
                    
                    String value = JSON.toJSONString(sendDemo);
                    ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

                    producer.send(record, new Callback() {
                        @Override
                        public void onCompletion(RecordMetadata metadata, Exception exception) {
                            if (exception == null) {
                                System.out.printf("Send succeed - Topic: %s, Partition: %d, Offset: %d%n",
                                        metadata.topic(), metadata.partition(), metadata.offset());
                            } else {
                                System.err.println("Send failed to topic " + metadata.topic() + ": " + exception.getMessage());
                            }
                        }
                    });
                }
                
                messageIndex++;
                System.out.println("Batch sent, total messages: " + messageIndex);
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            System.out.println("kafka producer error: " + e);
        } finally {
            producer.close();
        }
    }

}
