package io.axoniq.demo.gamerental.query;

import io.axoniq.demo.gamerental.coreapi.FindGameQuery;
import io.axoniq.demo.gamerental.coreapi.FullGameCatalogQuery;
import io.axoniq.demo.gamerental.coreapi.Game;
import io.axoniq.demo.gamerental.coreapi.GameRegisteredEvent;
import io.axoniq.demo.gamerental.coreapi.GameRentedEvent;
import io.axoniq.demo.gamerental.coreapi.GameReturnedEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@ProcessingGroup("game-catalog")
class GameCatalogProjector {

    private final GameViewRepository repository;

    public GameCatalogProjector(GameViewRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void on(GameRegisteredEvent event) {
        repository.save(new GameView(event.getGameIdentifier(),
                                     event.getTitle(),
                                     event.getReleaseDate(),
                                     event.getDescription(),
                                     event.isSingleplayer(),
                                     event.isMultiplayer()));
    }

    @EventHandler
    public void on(GameRentedEvent event) {
        Optional<GameView> result = repository.findById(event.getGameIdentifier());
        if (result.isPresent()) {
            result.get().decrementStock();
        } else {
            throw new IllegalArgumentException("Game with id [" + event.getGameIdentifier() + "] could no be found");
        }
    }

    @EventHandler
    public void on(GameReturnedEvent event) {
        Optional<GameView> result = repository.findById(event.getGameIdentifier());
        if (result.isPresent()) {
            result.get().incrementStock();
        } else {
            throw new IllegalArgumentException("Game with id [" + event.getGameIdentifier() + "] could no be found");
        }
    }

    @QueryHandler
    public List<String> handle(FullGameCatalogQuery query) {
        return repository.findAll().stream()
                         .map(GameView::getTitle)
                         .collect(Collectors.toList());
    }

    @QueryHandler
    public Game handle(FindGameQuery query) {
        String gameIdentifier = query.getGameIdentifier();
        return repository.findById(gameIdentifier)
                         .map(gameView -> new Game(
                                 gameView.getTitle(),
                                 gameView.getDescription(),
                                 gameView.isSingleplayer(),
                                 gameView.isMultiplayer()
                         ))
                         .orElseThrow(() -> new IllegalArgumentException(
                                 "Game with id [" + gameIdentifier + "] could no be found"
                         ));
    }
}