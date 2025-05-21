package frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement;

import java.util.List;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import frba.utn.edu.ar.tp_tacs.api.telegramBot.sessionManagement.BotSession;

public class CommonButtons {

    public static InlineKeyboardMarkup displayCorrectOrIncorrectValueButtons() {
 
        var correct = InlineKeyboardButton.builder()
        .text("✅").callbackData("correct")           
        .build();

        var incorrect = InlineKeyboardButton.builder()
        .text("❌").callbackData("incorrect")           
        .build();

        InlineKeyboardMarkup menu = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(correct, incorrect)).build();

        return menu;
    }

    public static InlineKeyboardMarkup displayMenu(BotSession session) {

        var publish = InlineKeyboardButton.builder()
        .text("Publicar 🃏").callbackData("PUBLISH")           
        .build();

        var search = InlineKeyboardButton.builder()
        .text("Buscar 🃏").callbackData("SEARCH")           
        .build();

        var offer = InlineKeyboardButton.builder()
        .text("Ofertar 🃏").callbackData("OFFER")           
        .build();

        var myCards = InlineKeyboardButton.builder()
        .text("Ver mis cartas 🃏").callbackData("MY_CARDS")           
        .build();

        var receivedOffers = InlineKeyboardButton.builder()
        .text("Ver ofertas 🃏").callbackData("RECEIVED_OFFERS")           
        .build();

        return InlineKeyboardMarkup.builder()
        .keyboard(List.of(
            List.of(publish),
            List.of(search),
            List.of(offer),
            List.of(myCards),
            List.of(receivedOffers)
        )).build();
    }
}
