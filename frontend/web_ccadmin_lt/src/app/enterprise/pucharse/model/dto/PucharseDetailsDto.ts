import { PucharseDetEntity } from "../entity/PucharseDetEntity";
import { PucharseHeadEntity } from "../entity/PucharseHeadEntity";

export class PucharseDetailsDto {
    
    public Headboard: PucharseHeadEntity;
    public DetailList: PucharseDetEntity[];
  
    constructor() {
      this.Headboard = new PucharseHeadEntity();
      this.DetailList = [];
    }
  }
