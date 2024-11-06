package ng;

import java.io.Serializable;

public class String implements Serializable, Comparable<String>, CharSequence {

    @Override
    public int compareTo(String o) {
        return 0;
    }
}