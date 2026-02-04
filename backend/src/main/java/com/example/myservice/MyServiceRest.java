package com.example.myservice;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MyServiceRest {

    private final MessageRepository messageRepository;

    public MyServiceRest(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @GetMapping("/api")
    public List<Message> getMessages() {
        return messageRepository.findAll();
    }

    @PostMapping("/api")
    public Message createMessage(@RequestBody Message message) {
        return messageRepository.save(message);
    }
}
