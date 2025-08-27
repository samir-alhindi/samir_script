import java.util.List;

interface SamirCallable {
    int arity();
    Object call(List<Object> arguments);
}

interface Subscriptable {
    Object get_item(Object index);
    Object set_item(Object index, Object item);
}

    
