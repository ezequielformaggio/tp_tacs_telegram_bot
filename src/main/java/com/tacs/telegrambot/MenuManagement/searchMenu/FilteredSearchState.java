package frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.searchMenu;

public enum FilteredSearchState {
    // TODO este archivo y el de state de publish son iguales, refactorizar
    NONE, AWAITING_GAME_NAME, AWAITING_CARD_NAME, AWAITING_CARD_STATE, AWAITING_IMAGES, 
    AWAITING_CARD_MIN_PRICE, AWAITING_CARD_MAX_PRICE, READY_TO_SEARCH, SEARCHING
}
