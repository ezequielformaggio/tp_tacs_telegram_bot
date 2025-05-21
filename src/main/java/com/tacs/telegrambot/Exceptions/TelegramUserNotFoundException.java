package frba.utn.edu.ar.tp_tacs.api.telegramBot.exceptions;

public class TelegramUserNotFoundException extends RuntimeException {

    public TelegramUserNotFoundException() {
        super("Telegram user not found in current session");
    }
    
}
