package com.ourexists.omes.message.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class NotifyDto {

    protected String id;

    protected String title;

    protected String context;

    protected Integer type;

    protected List<String> platforms;

    protected Integer step;

    private String source;

    private String sourceId;

    /** stream 出站生成的幂等键，供消费端 / Kafka 去重。 */
    private String eventId;
}
