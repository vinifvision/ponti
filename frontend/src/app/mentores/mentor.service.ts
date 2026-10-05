import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { CrudService } from '../crud.service';
import { Mentor } from '../models';

@Injectable({ providedIn: 'root' })
export class MentorService extends CrudService<Mentor> {
  constructor(http: HttpClient) {
    super(http, 'mentores');
  }
}
