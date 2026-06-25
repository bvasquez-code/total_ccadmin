export class ObjectUtil {

    static nvl<T>(text: T | null | undefined, value: T): T {
        return text ? text : value;
    }


}