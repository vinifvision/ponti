import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { CrudService } from '../crud.service';
import { Startup } from '../models';

@Injectable({ providedIn: 'root' })
export class StartupService extends CrudService<Startup> {
  constructor(http: HttpClient) {
    super(http, 'startups');
  }
}
