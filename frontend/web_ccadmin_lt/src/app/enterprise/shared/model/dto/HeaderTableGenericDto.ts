import { OptionTableGenericDto } from "./OptionTableGenericDto";

export class HeaderTableGenericDto
{
    /** Todas las columnas son visibles en escritorio por defecto. */
    public VisibleDesktop?: boolean;
    /** En móvil se muestran por defecto los dos primeros datos y las opciones. */
    public VisibleMobile?: boolean;
    /** El detalle incluye los campos habilitados en alguna vista, salvo indicación explícita. */
    public VisibleInDetails?: boolean;
    public Name? : string = "";
    public key? : string = "";
    public ColumnAction? : boolean = false;
    public Id? : string[] = [];
    public IsStatus? : boolean = false;
    public IsDate? : boolean = false;
    public IsProgress? : boolean = false;
    public Options? :OptionTableGenericDto[] = [];
    public Html? : any;
    public Mask? : any = "";
    public IsMoney? : boolean = false;
    public FunctionKey?: (...args: any[]) => any;
    public CellClassFunction?: (...args: any[]) => string;
    public ProgressTextFunction?: (...args: any[]) => string;
    public ProgressClassFunction?: (...args: any[]) => string;

    public constructor()
    {

    }


}
