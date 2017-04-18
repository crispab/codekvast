import {Injectable, Pipe, PipeTransform} from '@angular/core';
import {isArray, isString} from 'util';

@Pipe({name: 'ckInvocationStatus'}) @Injectable()
export class InvocationStatusPipe implements PipeTransform {

    transform(value: any): string {
        if (isString(value)) {
            return this.prettyPrint(value);
        }
        if (isArray(value)) {
            return value.sort().map((v: any) => {
                return isString(v) ? this.prettyPrint(v) : v;
            }).join(', ')
        }
        return value;
    }

    private prettyPrint(v: string) {
        return v.substr(0, 1).toUpperCase() + v.substr(1).toLowerCase().replace(/_/g, ' ');
    }
}
