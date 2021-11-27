import {Injectable, Pipe, PipeTransform} from '@angular/core';

@Pipe({name: 'ckInvocationStatus'}) @Injectable()
export class InvocationStatusPipe implements PipeTransform {

    transform(value: any): string {
        if (typeof value === 'string') {
            return this.prettyPrint(value);
        }
        if (Array.isArray(value)) {
            return value.sort().map((v: any) => typeof v === 'string' ? this.prettyPrint(v) : v == null ? null : v.toString()).join(', ');
        }
        return value == null ? null : value.toString();
    }

    private prettyPrint(v: string) {
        return v.substr(0, 1).toUpperCase() + v.substr(1).toLowerCase().replace(/_/g, ' ');
    }
}
