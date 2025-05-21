package frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.searchMenu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.google.common.net.HttpHeaders;

import frba.utn.edu.ar.tp_tacs.api.telegramBot.TelegramBot;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.exceptions.ImageManagingException;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.CommonButtons;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.sessionManagement.BotSession;
import frba.utn.edu.ar.tp_tacs.enums.ConservationStatus;
import frba.utn.edu.ar.tp_tacs.enums.Game;
import frba.utn.edu.ar.tp_tacs.interfaces.dto.CardDTO;
import frba.utn.edu.ar.tp_tacs.interfaces.dto.CardFilter;

@Component
public class SearchManager {
    
    private TelegramBot bot;

    private final WebClient webClient;

    public void setBot(TelegramBot bot) {
        this.bot = bot;
    }

    public SearchManager(WebClient webClient) {
        this.webClient = webClient;
    }

    //TODO refactorizarla logica de esta clase y la de PublishManager
    public void displaySearchManagerMenu(BotSession session, Update update) {

        createFilteredSearchIfNotExists(session, update);

        if(isFilteredSearchState(session, FilteredSearchState.NONE)) {

            bot.sendMessage(session, "Perfecto, ¡vamos a buscar cartas!");
            displayGameSelectionMenu(session, update);

        } else if(isFilteredSearchState(session, FilteredSearchState.AWAITING_GAME_NAME)) {

            receiveGameName(session, update);
            displayNameSelectionMenu(session, update);
            
        } else if (isFilteredSearchState(session, FilteredSearchState.AWAITING_CARD_NAME)) {

            receiveCardName(session, update);
            displayCardStateSelectionMenu(session, update);
   
        } else if (isFilteredSearchState(session, FilteredSearchState.AWAITING_CARD_STATE)) {

            receiveCardState(session, update);
            displayImageMenu(session, update);

        } else if (isFilteredSearchState(session, FilteredSearchState.AWAITING_IMAGES)) {

            receiveCardImages(session, update);
            displayCardPriceMenu(session, update); 

        } else if (isFilteredSearchState(session, FilteredSearchState.AWAITING_CARD_MIN_PRICE)
            || isFilteredSearchState(session, FilteredSearchState.AWAITING_CARD_MAX_PRICE)) {

            receiveCardPrice(session, update);
            displayConfirmationFilteredSearchMenu(session, update);
            
        } else if (session.getFilteredSearch().getFilteredSearchState().equals(FilteredSearchState.READY_TO_SEARCH)) {

            manageFilteredSearchFinalStep(session, update);

        } else if (session.getFilteredSearch().getFilteredSearchState().equals(FilteredSearchState.SEARCHING)) {

            manageFilteredSearchFinalStep(session, update);
            manageSearch(session, update);

        }
    }

    private void manageSearch(BotSession session, Update update) {
        if(update.hasMessage() && update.getMessage().getText().equals("siguiente")) {
            session.getFilteredSearch().increaseCurrentIndex();
            displaySearchedCard(session);
        } else {
            session.getFilteredSearch().decreaseCurrentIndex();
            displaySearchedCard(session);
        }
    }

    private boolean isFilteredSearchState(BotSession session, FilteredSearchState state) {
        return session.getFilteredSearch().getFilteredSearchState().equals(state);
    }

    public void createFilteredSearchIfNotExists(BotSession session, Update update) {
        if(session.getFilteredSearch() == null) {
            session.setFilteredSearch(new FilteredSearch());
            setStateTo(session, FilteredSearchState.NONE);
            session.getFilteredSearch().setCardFilter(new CardFilter());
        }
    }

    public void displayGameSelectionMenu(BotSession session, Update update) {

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    
        for (Game game : Game.values()) {
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(game.toString())
                .callbackData(game.toString())
                .build();
    
            keyboard.add(List.of(button));
        }

        InlineKeyboardButton omitir = InlineKeyboardButton.builder()
                .text("OMITIR")
                .callbackData("omitir")
                .build();

        keyboard.add(List.of(omitir));

        InlineKeyboardMarkup menu = InlineKeyboardMarkup.builder()
            .keyboard(keyboard)
            .build();
    
        setStateTo(session, FilteredSearchState.AWAITING_GAME_NAME);
        bot.sendMenu(session, "Por favor, indique el juego al que pertenece la carta o presione omitir", menu);
    }
    
    private void displayNameSelectionMenu(BotSession session, Update update) {
        if(userPressedAButton(update)) {
            setStateTo(session, FilteredSearchState.AWAITING_CARD_NAME);
            bot.sendMessage(session, "¡Bien!, Ahora por favor indique el nombre de la carta o escriba \"omitir\"");
        }
    }

    private void displayCardStateSelectionMenu(BotSession session, Update update) {
        if((userPressedAButton(update) && buttonHasLabel(update, "correct"))
            || (update.hasMessage() && update.getMessage().getText().equals("omitir"))) {
            setStateTo(session, FilteredSearchState.AWAITING_CARD_STATE);
            handleCardStatusSelectionMenu(session, update);
        } 
    }
 
    private void displayImageMenu(BotSession session, Update update) {
        if(userPressedAButton(update)) {
            setStateTo(session, FilteredSearchState.AWAITING_IMAGES);
            bot.sendMessage(session, "¡Bien!, Ahora por favor adjunte las imagenes de la carta y escriba \"listo\"");
            bot.sendMessage(session, "O escriba \"omitir\" si desea continuar sin mandar imagenes");
            bot.sendMessage(session, "Si se envio una imagen no deseada, simplemente escriba \"/retry\" y envie las imagenes nuevamente");
        }
    }
    
    private void displayCardPriceMenu(BotSession session, Update update) {
        if (userOmmited(update)) {
            setStateTo(session, FilteredSearchState.AWAITING_CARD_MIN_PRICE);
            bot.sendMessage(session, "¡Bien!, Ahora por favor escriba el valor minimo esperado de la carta o escriba \"omitir\" para saltear al siguiente paso");
        }
    }

    public Boolean userOmmited(Update update) {
        return update.hasMessage() && 
               !update.getMessage().hasPhoto() && 
               (update.getMessage().getText().toLowerCase().equals("listo") || 
               update.getMessage().getText().toLowerCase().equals("omitir"));
    }

    private void displayConfirmationFilteredSearchMenu(BotSession session, Update update) {
        if(isFilteredSearchState(session, FilteredSearchState.AWAITING_CARD_MIN_PRICE)) {
            setStateTo(session, FilteredSearchState.AWAITING_CARD_MAX_PRICE);
            if(update.hasMessage() && (update.hasMessage() && update.getMessage().getText().equals("omitir"))) {
                bot.sendMessage(session, "¡Bien!, Ahora por favor escriba el valor maximo esperado de la carta o escriba \"omitir\" para saltear al siguiente paso");
            }
        } else {
            if(!update.hasMessage()) {
                if(userPressedAButton(update) && buttonHasLabel(update, "correct")) { 
                   if(session.getFilteredSearch().getMaxValueDone()) {
                        displayFilteredSearchMenu(session);
                    } else {
                        bot.sendMessage(session, "¡Bien!, Ahora por favor escriba el valor maximo esperado de la carta o escriba \"omitir\" para saltear al siguiente paso");
                    }
                } 
            } else if ((update.hasMessage())) {
                if(update.getMessage().getText().toLowerCase().equals("omitir") && session.getFilteredSearch().getMaxValueDone()) {
                    displayFilteredSearchMenu(session);
                }
            }
        }
    }

    public void displayFilteredSearchMenu(BotSession session) {
        if(!isFilteredSearchState(session, FilteredSearchState.AWAITING_CARD_MIN_PRICE)) {
            setStateTo(session, FilteredSearchState.READY_TO_SEARCH);
            displaySelectedSerarchParameters(session);
            bot.sendMenu(session, "¿Desea buscar cartas con estos parametros?", CommonButtons.displayCorrectOrIncorrectValueButtons());
        }
    }

    private void displaySelectedSerarchParameters(BotSession session) {
        CardFilter card = session.getFilteredSearch().getCardFilter();
        String name = card.getName() == null ? " -" : card.getName();
        String minPrice = card.getMinPrice() == null ? " -" : card.getMinPrice().toString();
        String maxPrice = card.getMaxPrice() == null ? " -" : card.getMaxPrice().toString();
        String game = card.getGame() == null ? "-" : card.getGame().toString();
        String state = card.getConservationStatus() == null ? " -" : card.getConservationStatus().toString();

        bot.sendMessage(session, "¡Bien!, vamos a revisar los detalles de la carta 🃏:");
        bot.sendMessage(session, "Nombre: " + name + "\n" 
            + "Juego: " + game + "\n"
            + "Estado de conservación: " + state + "\n"
            + "Precio mínimo: $" + minPrice + "\n"
            + "Precio máximo: $" + maxPrice);
        if(card.getImages() != null) {
            for(String fileId : card.getImages()) {
                SendPhoto photo = new SendPhoto();
                photo.setChatId(session.getUserId().toString());
                photo.setPhoto(new InputFile(fileId));  
                try {
                    bot.execute(photo);
                } catch (TelegramApiException e) {
                    throw new ImageManagingException("Error al enviar las imagenes " + e);
                }
            }
        }
    }

    private void receiveGameName(BotSession session, Update update) {
        if(userPressedAButton(update) && !buttonHasLabel(update, "omitir")) {
            Game game = Game.valueOf(pressedButtonValue(update));
            session.getFilteredSearch().getCardFilter().setGame(game);
        }
    }
    
    private void receiveCardName(BotSession session, Update update) {
        if(update.hasMessage()) {
            if(!update.getMessage().getText().equals("omitir")) {
                String name = update.getMessage().getText();
                session.getFilteredSearch().getCardFilter().setName(name);
                bot.sendMenu(session, "El valor ingresado, " + name + ", es correcto?", CommonButtons.displayCorrectOrIncorrectValueButtons());
            }
        }
        handleIncorrectValue(session, update);
    }

    private void receiveCardState(BotSession session, Update update) {
        if(userPressedAButton(update)) {
            if(!buttonHasLabel(update, "omitir")) {
                ConservationStatus status = ConservationStatus.valueOf(pressedButtonValue(update));
                session.getFilteredSearch().getCardFilter().setConservationStatus(status);
            }
        }
    }

    private void receiveCardImages(BotSession session, Update update) {
        if(update.hasMessage() && !update.getMessage().hasPhoto() && update.getMessage().getText().equals("/retry")) {
            bot.sendMessage(session, "Perfecto, envie las imagenes nuevamente");
            session.getFilteredSearch().getCardFilter().setImages(new ArrayList<>());
        }
        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            List<PhotoSize> photos = update.getMessage().getPhoto();
            PhotoSize largest = photos.get(photos.size() - 1);
            String fileId = largest.getFileId();
            session.getFilteredSearch().getCardFilter().getImages().add(fileId);       
        }
    }

    private void receiveCardPrice(BotSession session, Update update) {
        if (update.hasMessage() && update.getMessage().getText().toLowerCase().equals("omitir")
            && !session.getFilteredSearch().getFilteredSearchState().equals(FilteredSearchState.AWAITING_CARD_MIN_PRICE)) {
            setStateTo(session, FilteredSearchState.READY_TO_SEARCH);
            session.getFilteredSearch().setMaxValueDone(true);
        }
        if(update.hasMessage() && !update.getMessage().getText().toLowerCase().equals("omitir")) {
            String price = update.getMessage().getText();
            price = price.replace("$", "").trim();
            if(price.matches("\\d+(\\.\\d+)?")) {
                Double formattedPrice = Double.valueOf(price);
                if(session.getFilteredSearch().getFilteredSearchState().equals(FilteredSearchState.AWAITING_CARD_MIN_PRICE)) {
                    session.getFilteredSearch().getCardFilter().setMinPrice(formattedPrice);
                } else if ((session.getFilteredSearch().getFilteredSearchState().equals(FilteredSearchState.AWAITING_CARD_MAX_PRICE))) {
                    session.getFilteredSearch().getCardFilter().setMaxPrice(formattedPrice);
                    session.getFilteredSearch().setMaxValueDone(true);
                }
                bot.sendMenu(session, "El valor ingresado, " + price + ", es correcto?", CommonButtons.displayCorrectOrIncorrectValueButtons());
            } else {
                bot.sendMessage(session, "Por favor ingrese solamente un valor numerico con formato decimal");
            }
        } 
        handleIncorrectValue(session, update);
    }

    private void manageFilteredSearchFinalStep(BotSession session, Update update) {
        if((userPressedAButton(update) && buttonHasLabel(update, "correct"))
            || (update.hasMessage() && update.getMessage().getText().toLowerCase().equals("omitir"))) {
            try {
                CardFilter card = session.getFilteredSearch().getCardFilter();
                Double minPrice = card.getMinPrice();
                Double maxPrice = card.getMaxPrice();
                card.setMinPrice(minPrice);
                card.setMaxPrice(maxPrice);
                session.getFilteredSearch().setSearchResults(searchCards(card, session));

                bot.sendMessage(session, "¡Busqueda realizada con exito!");
                bot.sendMessage(session, "Se han encontrado un total de " + session.getFilteredSearch().getSearchResults().size() + " cartas");
                bot.sendMessage(session, "A continuación vamos a ver los resultados");
                bot.sendMessage(session, "Para parar con la busqueda y volver al menu principal escriba \"/menu\"");
                bot.sendMessage(session, "Para ver la carta siguiente escriba \"siguiente\"");
                bot.sendMessage(session, "Para ver la carta anterior escriba \"anterior\"");
                setStateTo(session, FilteredSearchState.SEARCHING);

                displaySearchedCard(session);

            } catch( Exception e ) {
                System.out.print(e);
                session.setLogged(false);
            }
        } else {
            if(!update.hasMessage()) {
                bot.sendMessage(session, "Por favor ingrese el valor nuevamente");
            }
        }
    }

    private void displaySearchedCard(BotSession session) {
        if(session.getFilteredSearch().getCurrentIndex() > session.getFilteredSearch().getSearchResults().size()-1) {
            bot.sendMessage(session, "No hay una carta siguiente, has llegado al final de la lista");
            session.getFilteredSearch().decreaseCurrentIndex();
        } else if (session.getFilteredSearch().getCurrentIndex() < 0) {
            bot.sendMessage(session, "No hay una carta anterior, has llegado al principio de la lista");
            session.getFilteredSearch().increaseCurrentIndex();
        } else {
            CardDTO card = session.getFilteredSearch().getSearchResults().get(session.getFilteredSearch().getCurrentIndex());
            bot.sendMessage(session, "🃏🃏🃏---------- Carta N°" + session.getFilteredSearch().getCurrentIndex() + " de " + session.getFilteredSearch().getSearchResults().size() + "----------🃏🃏🃏");
            bot.sendMessage(session, "Nombre: " + card.getName() + "\n" 
                        + "Juego: " + card.getGame() + "\n"
                        + "Estado de conservación: " + card.getState() + "\n"
                        + "Precio mínimo: $" + card.getPrice() + "\n");
                    if(card.getImages() != null) {
                        Boolean errorEnImagen = false;
                        for(String fileId : card.getImages()) {
                            SendPhoto photo = new SendPhoto();
                            photo.setChatId(session.getUserId().toString());
                            File localImage = new File("C:/Users/forma/Desktop/TACS/TP-TACS/src/main/resources/static" + fileId);


                            if (localImage.exists()) {
                                photo.setPhoto(new InputFile(localImage));
                            } else {
                                photo.setPhoto(new InputFile(fileId)); 
                            }
                             
                            try {
                                bot.execute(photo);
                            } catch (TelegramApiException e) {
                                errorEnImagen = true;
                            }
                        }
                        if(errorEnImagen) { 
                            bot.sendMessage(session, "Ocurrio un error al mostrar una o mas imagenes");
                        }
                    }
        }
        
    }

    public void handleCardStatusSelectionMenu(BotSession session, Update update) {

        List<InlineKeyboardButton> buttons = new ArrayList<>();

        for(ConservationStatus status : ConservationStatus.values()) {
            buttons.add(InlineKeyboardButton.builder()
            .text(status.toString()).callbackData(status.toString())           
            .build());
        }

        InlineKeyboardButton omitir = InlineKeyboardButton.builder()
                .text("OMITIR")
                .callbackData("omitir")
                .build();

        buttons.add(omitir);

        InlineKeyboardMarkup menu = InlineKeyboardMarkup.builder()
            .keyboardRow(buttons).build();

        bot.sendMenu(session, "¡Bien!, Ahora por favor indique el estado de conservación de la carta", menu);
    }

    public void handleIncorrectValue(BotSession session, Update update) {
        if(userPressedAButton(update) && buttonHasLabel(update, "incorrect")) {
            bot.sendMessage(session, "Por favor ingrese el valor nuevamente");
        }
    }

    public List<CardDTO> searchCards(CardFilter cardFilter, BotSession session) {
        
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/cards");
    
        if (cardFilter.getName() != null) uriBuilder.queryParam("name", cardFilter.getName());
        if (cardFilter.getGame() != null) uriBuilder.queryParam("game", cardFilter.getGame());
        if (cardFilter.getConservationStatus() != null) uriBuilder.queryParam("conservationStatus", cardFilter.getConservationStatus());
        if (cardFilter.getMinPrice() != null) uriBuilder.queryParam("minPrice", cardFilter.getMinPrice());
        if (cardFilter.getMaxPrice() != null) uriBuilder.queryParam("maxPrice", cardFilter.getMaxPrice());
        if (cardFilter.getImages() != null) {
        for (String imageId : cardFilter.getImages()) {
            uriBuilder.queryParam("images", imageId);
        }
    }

        return webClient.get()
            .uri(uriBuilder.toUriString())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.getAuthToken())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<CardDTO>>() {})
            .block();
    }

    public void setStateTo(BotSession session, FilteredSearchState state) {
        session.getFilteredSearch().setFilteredSearchState(state);
    }

    public Boolean userPressedAButton(Update update) {
        return update.hasCallbackQuery();
    }

    public Boolean buttonHasLabel(Update update, String label) {
        return update.getCallbackQuery().getData().equals(label);
    }

    public String pressedButtonValue(Update update) {
        return update.getCallbackQuery().getData();
    }
}

