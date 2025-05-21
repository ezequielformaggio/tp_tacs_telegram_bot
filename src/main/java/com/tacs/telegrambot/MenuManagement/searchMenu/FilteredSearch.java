package frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.searchMenu;

import java.util.ArrayList;
import java.util.List;

import frba.utn.edu.ar.tp_tacs.interfaces.dto.CardDTO;
import frba.utn.edu.ar.tp_tacs.interfaces.dto.CardFilter;
import lombok.Data;

@Data
public class FilteredSearch {
    CardFilter cardFilter;
    FilteredSearchState filteredSearchState;
    Boolean maxValueDone;
    List<CardDTO> searchResults = new ArrayList<>();
    int currentIndex;

    public FilteredSearch() {
        this.maxValueDone = false;
        this.currentIndex = 0;
    }

    public void increaseCurrentIndex() {
            this.currentIndex++;
    }

    public void decreaseCurrentIndex() {
        this.currentIndex--;
    }
}
