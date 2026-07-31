package br.gov.crateus.bcm.saged.infrastructure.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TelegramUpdate {

    @JsonProperty("update_id")
    private Long updateId;

    @JsonProperty("message")
    private Message message;

    @JsonProperty("callback_query")
    private CallbackQuery callbackQuery;

    public Long getUpdateId()          { return updateId; }
    public Message getMessage()        { return message; }
    public CallbackQuery getCallbackQuery() { return callbackQuery; }

    public static class Message {
        @JsonProperty("message_id")   private Long messageId;
        @JsonProperty("from")         private From from;
        @JsonProperty("chat")         private Chat chat;
        @JsonProperty("text")         private String text;
        @JsonProperty("date")         private Long date;
        @JsonProperty("contact")      private Contact contact;
        @JsonProperty("web_app_data") private WebAppData webAppData;

        public Long getMessageId()        { return messageId; }
        public From getFrom()             { return from; }
        public Chat getChat()             { return chat; }
        public String getText()           { return text; }
        public Long getDate()             { return date; }
        public Contact getContact()       { return contact; }
        public WebAppData getWebAppData() { return webAppData; }
    }

    public static class WebAppData {
        @JsonProperty("data")        private String data;
        @JsonProperty("button_text") private String buttonText;
        public String getData()       { return data; }
        public String getButtonText() { return buttonText; }
    }

    public static class CallbackQuery {
        @JsonProperty("id")      private String id;
        @JsonProperty("from")    private From from;
        @JsonProperty("message") private Message message;
        @JsonProperty("data")    private String data;

        public String getId()      { return id; }
        public From getFrom()      { return from; }
        public Message getMessage(){ return message; }
        public String getData()    { return data; }
    }

    public static class Contact {
        @JsonProperty("phone_number") private String phoneNumber;
        @JsonProperty("first_name")   private String firstName;
        @JsonProperty("user_id")      private Long userId;

        public String getPhoneNumber() { return phoneNumber; }
        public String getFirstName()   { return firstName; }
        public Long getUserId()        { return userId; }
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

        public Long getId()     { return id; }
        public String getType() { return type; }
    }
}
