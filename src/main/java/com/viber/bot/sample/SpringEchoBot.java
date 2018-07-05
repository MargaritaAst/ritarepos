package com.viber.bot.sample;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.Futures;
import com.viber.bot.Request;
import com.viber.bot.ViberSignatureValidator;
import com.viber.bot.api.ViberBot;
import com.viber.bot.message.Message;
import com.viber.bot.message.TextMessage;
import com.viber.bot.profile.BotProfile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;


import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@SpringBootApplication
public class SpringEchoBot implements ApplicationListener<ApplicationReadyEvent> {

    @Inject
    private ViberBot bot;



    @Inject

    private ViberSignatureValidator signatureValidator;

    @Value("${application.viber-bot.webhook-url}")
    private String webhookUrl;

    public static void main(String[] args) {
        SpringApplication.run(SpringEchoBot.class, args);
    }

    public  String mass()
    {
        //создадим массив слов
        String [] massiv = {"-Роман Николаевич, это вы? " ,
                "-поставьте зачёт, пожалуйста ",
                "-я хочу стать мороженщиком ",
                "- меня уважают во всех фаст-фудах страны",
        "-я у мамы программистышь"};
        //получим случаное число
        Integer rand =  Math.abs(new Random(new Date().getTime()).nextInt()) % massiv.length;
        String wordrand = (massiv[rand]);
        return wordrand;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent appReadyEvent) {
        try {
            bot.setWebhook(webhookUrl).get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //событие напишет случайное слово из массива если пользвоатель напишет что-нибудь в чат
        bot.onMessageReceived((event, message, response) -> response.send(mass()));
        //собыстие напишет привет при входе пользователя в чат
        bot.onConversationStarted(event -> Futures.immediateFuture(Optional.of(
                new TextMessage("Привет,я робот!" + event.getUser().getName()))));
    }

    @PostMapping(value = "/", produces = "application/json")
    public String incoming(@RequestBody String json,
                           @RequestHeader("X-Viber-Content-Signature") String serverSideSignature)
            throws ExecutionException, InterruptedException, IOException {
        Preconditions.checkState(signatureValidator.isSignatureValid(serverSideSignature, json), "invalid signature");
        @Nullable InputStream response = bot.incoming(Request.fromJsonString(json)).get();
        return response != null ? CharStreams.toString(new InputStreamReader(response, Charsets.UTF_16)) : null;
    }

    @Configuration
    public class BotConfiguration {

        @Value("${application.viber-bot.auth-token}")
        private String authToken;

        @Value("${application.viber-bot.name}")
        private String name;

        @Nullable
        @Value("${application.viber-bot.avatar:@null}")
        private String avatar;



        @Bean
        ViberBot viberBot() {
            return new ViberBot(new BotProfile(name, avatar), authToken);
        }

        @Bean
        ViberSignatureValidator signatureValidator() {
            return new ViberSignatureValidator(authToken);
        }
    }
}
