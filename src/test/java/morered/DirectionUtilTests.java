package morered;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import commoble.morered.util.DirectionHelper;
import net.minecraft.util.Direction;

public class DirectionUtilTests
{
	@Test
	void testDirectionCompression()
	{
		int previousCompressed = 0;
		boolean doContinuityTests = false;
		for (int side = 0; side < 6; side++)
		{
			for (int subSide = 0; subSide < 6; subSide++)
			{
				Direction primaryDir = Direction.byIndex(side);
				Direction secondaryDir = Direction.byIndex(subSide);
				if (primaryDir.getAxis() != secondaryDir.getAxis())
				{
//					System.out.println(String.format("testing %d, %d", side,subSide));
					int compressed = DirectionHelper.getCompressedSecondSide(side, subSide);
					int uncompressed = DirectionHelper.uncompressSecondSide(side, compressed);
					Assertions.assertEquals(secondaryDir, Direction.byIndex(uncompressed));
					if (doContinuityTests)
					{
						Assertions.assertEquals((previousCompressed+1) % 4, compressed);
					}

					previousCompressed = compressed;
					doContinuityTests = true;
				}
			}
		}
	}
}
