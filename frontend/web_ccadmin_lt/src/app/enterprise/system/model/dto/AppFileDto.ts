export class AppFileDto {

    public base64 : string;

    public extension : string;

    public type : string;

    public groupTypeFile : number;

    constructor(){
        this.base64 = "";
        this.extension = "";
        this.type = "";
        this.groupTypeFile = 0;
    }
}
