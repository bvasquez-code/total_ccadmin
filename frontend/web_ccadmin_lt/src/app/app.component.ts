import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  
  title = 'ccadmin2';

  ExisteSesion : boolean = ( sessionStorage.getItem('Token') ) ? true : false;


  getSession(){

    let ExistSesion : boolean = ( sessionStorage.getItem('Token') ) ? true : false;

    return ExistSesion;
    
  }


}
