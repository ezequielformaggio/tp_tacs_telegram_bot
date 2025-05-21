package frba.utn.edu.ar.tp_tacs.api.telegramBot.sessionManagement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import frba.utn.edu.ar.tp_tacs.api.telegramBot.TelegramBot;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.exceptions.TelegramUserNotFoundException;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.CommonButtons;
import frba.utn.edu.ar.tp_tacs.interfaces.dto.LoginRequestDTO;
import frba.utn.edu.ar.tp_tacs.interfaces.dto.LoginResponseDTO;

@Component
public class SessionManager {

    private final WebClient webClient;

    private List<BotSession> sessions = new ArrayList<>();

    private TelegramBot bot;

    public void setBot(TelegramBot bot) {
        this.bot = bot;
    }

    public SessionManager(WebClient webClient) {
        this.webClient = webClient;
    }

    public LoginResponseDTO callLoginEndpoint(String username, String password) {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO(username, password);
        return webClient.post()
        .uri("/auth/login")
        .bodyValue(loginRequestDTO)
        .retrieve()
        .bodyToMono(LoginResponseDTO.class)
        .block();
    }
    
    public boolean isLogged(Long userId) {
        return this.sessions.stream().map(s -> s.getUserId()).collect(Collectors.toList()).contains(userId);
    }

    public BotSession handleLogin(Update update) {

        BotSession session = getCurrentSession(update);

        if(session.getState().equals(SessionState.START)) {
            handleLoginMenu(update, session);
        } else if(session.getState().equals(SessionState.AWAITING_USERNAME)) {
            handleUsernameInput(update, session);
        } else if(session.getState().equals(SessionState.AWAITING_PASSWORD)) {
            handlePasswordInput(update, session);
        }

        return session;
    }

    
    public void handleUsernameInput(Update update, BotSession session) {

        if(!userPressedAButton(update)) {
                String username = update.getMessage().getText();
                session.setUsername(username);
                bot.sendMenu(session, "El nombre de usuario ingresado, " + username + ", es correcto?", CommonButtons.displayCorrectOrIncorrectValueButtons());
        } else {
            if(update.getCallbackQuery().getData().equals("correct")) {
                try {
                    session.setState(SessionState.AWAITING_PASSWORD);
                    bot.sendMessage(session, "Por favor, ingrese su contraseña");
                } catch(WebClientResponseException e) {
                    bot.sendMessage(session, "Hubo un problema al ingresar su contraseña");
                }
            } else {
                bot.sendMessage(session, "Por favor ingrese el valor nuevamente");
            }
        } 
    }

    public void handlePasswordInput(Update update, BotSession session) {
        if(!userPressedAButton(update)) {
            String password = update.getMessage().getText();
            session.setPassword(password);
            bot.sendMenu(session, "El password ingresado, " + password + ", es correcto?", CommonButtons.displayCorrectOrIncorrectValueButtons());
        } else {
            if(update.getCallbackQuery().getData().equals("correct")) {
                handleCredentials(session);
            } else {
                bot.sendMessage(session, "Por favor ingrese el valor nuevamente");
            }
        } 
    }

    public void handleCredentials(BotSession session) {
 
        try {
            LoginResponseDTO login = callLoginEndpoint(session.getUsername(),session.getPassword());
            System.out.println("TOKENN");
            System.out.println(login.getToken());
            session.setAuthToken(login.getToken());
            session.setUserSystemId(login.getUser().getId());
            session.setPassword(null);
            session.setLogged(true);
            bot.sendMessage(session, "Se ha iniciado sesion exitosamente");
            InlineKeyboardMarkup menu = CommonButtons.displayMenu(session);
            bot.sendMenu(session, "¿Que quieres hacer?", menu);
        } catch(RuntimeException e) {
            bot.sendMessage(session, "Usuario o contraseña incorrectos, por favor, vuelva a ingresar su usuario:");
            session.setState(SessionState.AWAITING_USERNAME);
        }
    }

    public void handleLoginMenu(Update update, BotSession session) {
        if(!userPressedAButton(update) && (update.hasMessage() && update.getMessage().getText().equals("/login"))) {
            bot.sendMessage(session, "Por favor, ingrese su nombre de usuario");
            session.setState(SessionState.AWAITING_USERNAME);
        }
    }

    public void handleUserInputValue(Update update, BotSession session) {

        if(!userPressedAButton(update)) {
            if(session.getState().equals(SessionState.AWAITING_USERNAME)) {
                String username = update.getMessage().getText();
                session.setUsername(username);
                bot.sendMenu(session, "El valor ingresado, " + username + ", es correcto?", CommonButtons.displayCorrectOrIncorrectValueButtons());
            } else if (session.getState().equals(SessionState.AWAITING_PASSWORD)) {
                String password = update.getMessage().getText();
                session.setPassword(password);
                bot.sendMenu(session, "El valor ingresado, " + password + ", es correcto?", CommonButtons.displayCorrectOrIncorrectValueButtons());
            }
        } else {
            if(update.getCallbackQuery().getData().equals("correct")) {
                session.setState(SessionState.AWAITING_PASSWORD);

            } else {
                bot.sendMessage(session, "Por favor ingrese el valor nuevamente");
            }
        } 
    }

    public BotSession getCurrentSession(Update update) {
        Long userId = getUserId(update);
        BotSession session = sessions.stream().filter(s -> s.getUserId().equals(userId)).findAny().orElse(null);
        return session == null ? createNewSession(userId) : session;
    }

    public BotSession createNewSession(Long userId) {
        BotSession session = new BotSession(userId);
        session.setState(SessionState.START);
        this.sessions.add(session);
        sendWelcomeMessage(session);
        return session;
    }

    public void sendWelcomeMessage(BotSession session) {
        //TODO implement global commands https://core.telegram.org/bots/features#global-commands
        bot.sendMessage(session,"Bot iniciado");
        bot.sendMessage(session,"Para iniciar sesion escriba \"/login\"");
        bot.sendMessage(session,"Para cerrar sesion escriba \"/logout\"");
        bot.sendMessage(session,"Para abortar cualquier operacion y volver al menu principal escriba \"/menu\"");
    }

    public Long getUserId(Update update) {
        if(update.hasMessage()) {
            return update.getMessage().getFrom().getId();
        }else if(userPressedAButton(update)) {
            return update.getCallbackQuery().getFrom().getId();
        } else {
            throw new TelegramUserNotFoundException();
        }
    }

    public boolean userPressedAButton(Update update) {
        return update.hasCallbackQuery();
    }

    public void logout(BotSession session) {
        session.setLogged(false);
        this.sessions.remove(session);
    }

}
