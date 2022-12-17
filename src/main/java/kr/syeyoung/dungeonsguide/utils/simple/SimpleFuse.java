package kr.syeyoung.dungeonsguide.utils.simple;

/**
 * This class represents a simple boolean fuse
 * equivalent to
 * <pre>{@code
 *  boolean fuse;
 *  public onClick(){
 *      if(!fuse){
 *          fuse = true;
 *          // do work that should only be done once
 *      }
 *  }
 * }</pre>
 * this is a utility class for code clarity <br/>
 * @author Eryk Ruta
 */
public class SimpleFuse {
    volatile boolean state = false;

    /**
     * first time you call its true, then the fuse is blown
     */
    public boolean checkAndBlow(){
        if(!state) {
            state = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean isBlown(){
        return state;
    }

    public void blow(){
        state = true;
    }

}
