package com.smartresizer.smartresizer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/send-message")
    public String sendMessage(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("message") String message,
            RedirectAttributes redirectAttributes) {

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo("rokkumar77@gmail.com");
            mail.setSubject("New Contact Msg from: " + name);
            mail.setText("Sender Name: " + name + "\n" +
                    "Sender Email: " + email + "\n\n" +
                    "Message:\n" + message);

            mailSender.send(mail);

            redirectAttributes.addFlashAttribute("successMsg", "Message sent successfully! 🚀");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to send message. Try again!");
            e.printStackTrace();
        }
        return "redirect:/#contact";
    }
}