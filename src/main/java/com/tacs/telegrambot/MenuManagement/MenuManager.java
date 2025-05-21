package frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.TelegramBot;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.myCardsMenu.MyCardsManager;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.offerMenu.OfferManager;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.publishMenu.PublishManager;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.searchMenu.SearchManager;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.receivedOffersMenu.ReceivedOffersManager;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.sessionManagement.BotSession;

@Component
public class MenuManager {

    private TelegramBot bot;

    @Autowired
    private PublishManager publishManager;

    @Autowired
    private SearchManager searchManager;

    @Autowired
    private OfferManager offerManager;

    @Autowired
    private MyCardsManager myCardsManager;

    @Autowired
    private ReceivedOffersManager receivedOffersManager;

    public void setBot(TelegramBot bot) {
        this.bot = bot;
        publishManager.setBot(bot); 
        searchManager.setBot(bot);
        offerManager.setBot(bot);
        myCardsManager.setBot(bot);
        receivedOffersManager.setBot(bot);
    }

    public void manageMainMenu(BotSession session, Update update) {

        if(update.hasMessage() && update.getMessage().getText() != null) {
            if(update.getMessage().getText().equals("/menu")) {
                session.discardOngoingOperations();
            }
        }

        if(update.hasCallbackQuery()) {
            String menuState = update.getCallbackQuery().getData();
            if(menuState.equals(MenuState.PUBLISH.toString())) {
                session.setMenuState(MenuState.PUBLISH);
            } else if(menuState.equals(MenuState.SEARCH.toString())) {
                session.setMenuState(MenuState.SEARCH);
            } else if(menuState.equals(MenuState.OFFER.toString())) {
                session.setMenuState(MenuState.OFFER);
            } else if(menuState.equals(MenuState.MY_CARDS.toString())) {
                session.setMenuState(MenuState.MY_CARDS);
            } else if(menuState.equals(MenuState.RECEIVED_OFFERS.toString())) {
                session.setMenuState(MenuState.RECEIVED_OFFERS);
            }
        }

        if(session.getMenuState().equals(MenuState.NONE)) {
            InlineKeyboardMarkup menu = CommonButtons.displayMenu(session);
            bot.sendMenu(session, "¿Que quieres hacer?", menu);
        } else if(session.getMenuState().equals(MenuState.PUBLISH)) {
            publishManager.displayPublishMenu(session, update);
        } else if(session.getMenuState().equals(MenuState.SEARCH)) {
            searchManager.displaySearchManagerMenu(session, update);
        } else if(session.getMenuState().equals(MenuState.OFFER)) {
            // TODO implementar offerManager.
        } else if(session.getMenuState().equals(MenuState.MY_CARDS)) {
            // TODO implementar myCardsManager.
        } else if(session.getMenuState().equals(MenuState.RECEIVED_OFFERS)) {
            // TODO implementar receivedOffersManager.
        }
        
    }

}
