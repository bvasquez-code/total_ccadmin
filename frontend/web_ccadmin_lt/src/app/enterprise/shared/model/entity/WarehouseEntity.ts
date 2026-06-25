import { AuditTableEntity } from "./AuditTableEntity";

export class WarehouseEntity extends AuditTableEntity {
    
    public WarehouseCod: string;
    public StoreCod: string;
    public WarehouseName: string;
    public WarehouseDesc: string;
  
    constructor() {
      super();
      this.WarehouseCod = '';
      this.StoreCod = '';
      this.WarehouseName = '';
      this.WarehouseDesc = '';
    }
  }
