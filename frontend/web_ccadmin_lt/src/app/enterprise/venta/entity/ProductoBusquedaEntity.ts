export class ProductoBusquedaEntity {

    public cod_producto_fk: string = "";
	public cod_local_fk: string = "";
	public nom_producto: string = "";
	public des_producto: string = "";
	public num_stock_digital: number = 0;
	public num_stock_fisico: number = 0;
	public num_precio: number = 0;
	public flg_descuento: string = "";
	public num_precio_promo: number = 0;
	public num_porc_desc: number = 0;
	public num_mont_desc: number = 0;
	public cod_marca: string = "";
	public nom_marca: string = "";
	public cod_categoria: string = "";
	public nom_categoria: string = "";
	public cod_categoria_prd: string = "";
	public nom_categoria_prd: string = "";
	public num_tedencia: number = 0;
	public ruta_archivo: string = "";
	public nom_archivo: string = "";
	public cod_moneda: string = "";
	public sim_moneda: string = "";
	public fec_creacion: Date = new Date();
	public fec_modifica: Date = new Date();

    constructor()
    {

    }

}