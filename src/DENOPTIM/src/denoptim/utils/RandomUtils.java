/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
 * 
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package denoptim.utils;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Collection;

import org.apache.commons.math3.random.MersenneTwister;

/**
 * Toolbox for random number generation.
 */

public class RandomUtils
{
    private static long rndSeed = 0L;
    private static MersenneTwister mt = null;

//------------------------------------------------------------------------------

    private static void setSeed(long value)
    {
        rndSeed = value;
    }

//------------------------------------------------------------------------------

    public static long getSeed()
    {
        return rndSeed;
    }
    
//------------------------------------------------------------------------------

    public static void initialiseRNG()
    {
        initialiseSeed();
        mt = new MersenneTwister(rndSeed);
    }
    
//------------------------------------------------------------------------------

    public static void initialiseRNG(long seed)
    {
        setSeed(seed);
        mt = new MersenneTwister(rndSeed);
    }
    
//------------------------------------------------------------------------------

    public static MersenneTwister getRNG()
    {
        if (mt == null)
        {
            initialiseRNG();
        }
        return mt;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Chooses one member among the given collection. Works on either ordered
     * an unordered collections.
     */
    
    public static <T> T randomlyChooseOne(Collection<T> c)
    {
        int chosen = mt.nextInt(c.size());
        int i=0;
        T chosenObj = null;
        for (T o : c)
        {
            if (i == chosen)
            {
                chosenObj = o;
            }
            i++;
        }
        return chosenObj;
    }

//------------------------------------------------------------------------------

    private static void initialiseSeed()
    {
        SecureRandom sec = new SecureRandom();
        byte[] sbuf = sec.generateSeed(8);
        ByteBuffer bb = ByteBuffer.wrap(sbuf);
        rndSeed = bb.getLong();
    }

//------------------------------------------------------------------------------

    public static boolean nextBoolean(double prob)
    {
        if (prob == 0.0)
            return false;
        else if (prob == 1.0)
            return true;
        return mt.nextDouble() < prob;
    }

//------------------------------------------------------------------------------

}
