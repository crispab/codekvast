import {Injectable, Pipe, PipeTransform} from '@angular/core';
import {isString} from 'util';

@Pipe({name: 'ckInvocationStatus'}) @Injectable()
export class InvocationStatusPipe implements PipeTransform {

    transform(value: any): string {
        if (isString(value)) {
            let s = value.toString();
            return s.substr(0, 1).toUpperCase() + s.substr(1).toLowerCase().replace(/_/g, ' ');
        }
        return value;
    }
}
