
/**
 * Stores x and Y position data
 */
public record Position(int x, int y)
{
    /**
     * An extra constructor for Position records
     */
    public Position()
    {
        this(0, 0);
    }
}