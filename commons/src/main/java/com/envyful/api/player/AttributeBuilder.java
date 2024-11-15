package com.envyful.api.player;

import com.envyful.api.player.attribute.AttributeTrigger;
import com.envyful.api.type.AsyncFunction;
import com.envyful.api.type.BiAsyncFunction;
import com.envyful.api.type.map.KeyedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AttributeBuilder<A extends Attribute<B>, B, C> {

    protected Class<A> attributeClass;
    protected Supplier<A> constructor;
    protected BiAsyncFunction<EnvyPlayer<C>, KeyedMap, Object> idMapper;
    protected List<BiPredicate<EnvyPlayer<C>, KeyedMap>> predicates = new ArrayList<>();
    protected List<AttributeTrigger<C>> triggers = new ArrayList<>();
    protected Function<UUID, B> offlineIdMapper = null;

    protected AttributeBuilder() {}

    public AttributeBuilder<A, B, C> attributeClass(Class<A> attributeClass) {
        this.attributeClass = attributeClass;
        return this;
    }

    public Class<A> attributeClass() {
        return this.attributeClass;
    }

    public AttributeBuilder<A, B, C> constructor(Supplier<A> constructor) {
        this.constructor = constructor;
        return this;
    }

    public AttributeBuilder<A, B, C> instantIdMapper(Function<EnvyPlayer<C>, Object> idMapper) {
        return this.idMapper(player -> CompletableFuture.completedFuture(idMapper.apply(player)));
    }

    public AttributeBuilder<A, B, C> idMapper(AsyncFunction<EnvyPlayer<C>, Object> idMapper) {
        this.idMapper = (cEnvyPlayer, keyedMap) -> idMapper.apply(cEnvyPlayer);
        return this;
    }

    public AttributeBuilder<A, B, C> idMapper(BiAsyncFunction<EnvyPlayer<C>, KeyedMap, Object> idMapper) {
        this.idMapper = idMapper;
        return this;
    }

    public AttributeBuilder<A, B, C> filter(Predicate<EnvyPlayer<C>> predicate) {
        this.predicates.add((cEnvyPlayer, keyedMap) -> predicate.test(cEnvyPlayer));
        return this;
    }

    public AttributeBuilder<A, B, C> filter(BiPredicate<EnvyPlayer<C>, KeyedMap> predicate) {
        this.predicates.add(predicate);
        return this;
    }

    @SafeVarargs
    public final AttributeBuilder<A, B, C> triggers(AttributeTrigger<C>... trigger) {
        this.triggers.addAll(List.of(trigger));
        return this;
    }

    public AttributeBuilder<A, B, C> offlineIdMapper(Function<UUID, B> offlineIdMapper) {
        this.offlineIdMapper = offlineIdMapper;
        return this;
    }

    public Function<UUID, B> offlineIdMapper() {
        return this.offlineIdMapper;
    }

    public void register(PlayerManager<?, C> playerManager) {
        playerManager.registerAttribute(this);
    }
}
