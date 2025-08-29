

public class SamirString extends SamirObject implements Subscriptable {
    StringBuilder string;
    Runtime runtime;
    SamirString(StringBuilder string, Runtime runtime){
        super(new Environment(runtime.environment), "string");
        this.string = string;
        this.runtime = runtime;
    }
    @Override
    public Object get_item(Object index) {
        int i = Util.checkValidIndex(index, string.length(), runtime);
        return string.charAt(i);
    }
    @Override
    public Object set_item(Object index, Object item, Token opp) {
        //int i = Util.checkValidIndex(index, string.length(), runtime);
        return "";

    }
}
