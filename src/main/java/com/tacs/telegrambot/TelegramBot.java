package frba.utn.edu.ar.tp_tacs.api.telegramBot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.MenuManager;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.sessionManagement.BotSession;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.sessionManagement.SessionManager;
import jakarta.annotation.PostConstruct;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    
    
    private String token;

    @Autowired
    private MenuManager menuManager;

    @Autowired
    private SessionManager sessionManager;


    public TelegramBot() throws IOException {
        String path = System.getenv("JWT_SECRET_KEY_FILE");
        this.token = Files.readString(Path.of(path));
    }

    @PostConstruct
    public void init() {
        sessionManager.setBot(this);
        menuManager.setBot(this);
    }

    @Override
    public String getBotUsername() {
        return "tp_tacs_g4_bot";
    }

    @Override
    public String getBotToken() {
        return token;
    }

    // Metodos para comunicarse directamente con el usuario en el chat de telegram
    public void sendMessage(BotSession session, String what){
        SendMessage sm = SendMessage.builder()
                         .chatId(session.getUserId().toString())
                         .text(what).build();
        try {
             execute(sm);                        
        } catch (TelegramApiException e) {
             throw new RuntimeException(e);
        }
     }

    public void sendMenu(BotSession session, String txt, InlineKeyboardMarkup kb){
        SendMessage sm = SendMessage.builder().chatId(session.getUserId().toString())
                .parseMode("HTML").text(txt)
                .replyMarkup(kb).build();
    
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {

        System.out.println("----- UPDATE RECIBIDO -----");
        System.out.println(update);

        BotSession session = sessionManager.getCurrentSession(update);

        if(update.hasMessage() && update.getMessage().getText() != null) {
            if(update.getMessage().getText().equals("/logout")) {
                sessionManager.logout(session);
            }
        }
   
        if(session.isLogged()) {
            menuManager.manageMainMenu(session, update);
        } else {
            sessionManager.handleLogin(update);
        }
    }

    

}