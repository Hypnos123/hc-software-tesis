import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { IItemMenu } from '../models/sidebar';
import { environment } from 'environments/environment';

@Injectable({
  providedIn: 'root'
})
export class MenuService {
  URLServicio: string = environment.URLTienda;

  constructor( private httpClient:HttpClient) { }

  getAllActive():Observable<IItemMenu[]> {
    return this.httpClient.get<IItemMenu[]>(`${this.URLServicio}menu/getAllActive`);
  }

}
