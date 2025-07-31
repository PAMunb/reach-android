package br.unb.cic.reach.common.analysis.strategy;

import br.unb.cic.reach.common.model.ConfigMatrix;

/**
 * Factory class for creating ReachabilityStrategyType instances based on configuration.
 * <p>
 * This factory is responsible for instantiating the appropriate reachability analysis
 * strategy based on the specified ReachabilityStrategyType in the configuration matrix.
 * The factory supports multiple implementations with different performance
 * characteristics and analysis capabilities.
 */
public class ReachabilityStrategyFactory {

    /**
     * Creates a ReachabilityStrategyType instance based on the provided configuration.
     *
     * @param configMatrix The configuration matrix containing analysis parameters
     * @return An initialized ReachabilityStrategyType instance
     * @throws IllegalArgumentException if the configuration is invalid or unsupported
     */
    public static ReachabilityStrategy createReachabilityStrategy(ConfigMatrix configMatrix) {
        if (configMatrix == null) {
            throw new IllegalArgumentException("ConfigMatrix cannot be null");
        }

        ConfigMatrix.ReachabilityStrategyType algorithm = configMatrix.getReachabilityStrategy();

        return switch (algorithm) {
            case JGRAPHT_DIJKSTRA -> new JGraphReachabilityStrategy();
            case SOOT_BFS -> new SootReachabilityStrategy();
        };
    }

}
