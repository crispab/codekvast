import {Component} from '@angular/core';
import {Method} from './model/Method';
import {AgePipe} from './age.pipe';
import {DatePipe} from '@angular/common';

@Component({
    selector: 'ck-method-detail',
    template: require('./method-detail.component.html'),
    providers: [AgePipe, DatePipe],
})
export class MethodDetailComponent {
    method: Method;
}
