package br.gov.crateus.bcm.saged.infrastructure.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TelegramUpdate {

    @JsonProperty("update_id")
    private Long updateId;

    @JsonProperty("message")
    private Message message;

    public Long getUpdateId() { return updateId; }
    public Message getMessage() { return message; }

    public static class Message {
        @JsonProperty("message_id") private Long messageId;
        @JsonProperty("from")       private From from;
        @JsonProperty("chat")       private Chat chat;
        @JsonProperty("text")       private String text;
        @JsonProperty("date")       private Long date;

        public Long getMessageId() { return messageId; }
        public From getFrom()      { return from; }
        public Chat getChat()      { return chat; }
        public String getText()    { return text; }
        public Long getDate()      { return date; }
    }

    public static class From {
        @JsonProperty("id")         private Long id;
        @JsonProperty("first_name") private String firstName;
        @JsonProperty("username")   private String username;

        public Long getId()          { return id; }
        public String getFirstName() { return firstName; }
        public String getUsername()  { return username; }
    }

    public static class Chat {
        @JsonProperty("id")   private Long id;
        @JsonProperty("type") private String type;

        public Long getId()    { return id; }
        public String getType(){ return type; }
    }
}
