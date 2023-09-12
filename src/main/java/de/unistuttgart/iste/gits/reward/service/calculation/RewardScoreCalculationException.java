package de.unistuttgart.iste.gits.reward.service.calculation;

/**
 * Exception thrown when an error occurs during the calculation of a reward score.
 */
public class RewardScoreCalculationException extends RuntimeException {

    public RewardScoreCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
