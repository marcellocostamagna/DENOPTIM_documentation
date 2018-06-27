package utils;

import java.io.Serializable;

/**
 * This class is the equivalent of the Pair data structure used in C++
 * Although <code>AbstractMap.SimpleImmutableEntry<K,V>>/code> is available
 * it does not have a setValue method. 
 * @author Vishwesh Venkatraman
 */
public class ObjectPair implements Serializable
{
    private Object o1;
    private Object o2;
    
//------------------------------------------------------------------------------    
    
    public ObjectPair()
    {
        o1 = null;
        o2 = null;
    }

//------------------------------------------------------------------------------    
    
    public ObjectPair(Object m_o1, Object m_o2) 
    { 
        o1 = m_o1; 
        o2 = m_o2; 
    }

//------------------------------------------------------------------------------
 
    public boolean isSame(Object o1, Object o2) 
    {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

//------------------------------------------------------------------------------
 
    public Object getFirst() 
    { 
        return o1;
    }
    
//------------------------------------------------------------------------------
    
    public Object getSecond() 
    { 
        return o2; 
    }
    
//------------------------------------------------------------------------------
 
    public void setFirst(Object o) 
    { 
        o1 = o; 
    }
    
//------------------------------------------------------------------------------
    
    public void setSecond(Object o) 
    { 
        o2 = o; 
    }
    
//------------------------------------------------------------------------------
 
    @Override
    public boolean equals(Object obj) 
    {
        if (obj == null) 
            return false;

        if (!(obj instanceof ObjectPair))
            return false;

        ObjectPair p = (ObjectPair)obj;
        return (isSame(p.o1, this.o1) && isSame(p.o2, this.o2));
    }
    
//------------------------------------------------------------------------------    
    
    @Override
    public int hashCode() 
    { 
        return ((o1 == null ? 0 : o1.hashCode()) ^ 
                                            (o2 == null ? 0 : o2.hashCode()));
    }

//------------------------------------------------------------------------------
 
    @Override
    public String toString()
    {
        return "DENOPTIMPair{"+o1+", "+o2+"}";
    }
    
//------------------------------------------------------------------------------    
 
}
