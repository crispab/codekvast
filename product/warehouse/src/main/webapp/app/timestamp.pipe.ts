import {Pipe, PipeTransform} from 'angular2/core';
import {DatePipe} from 'angular2/src/common/pipes/date_pipe';

@Pipe({name: 'ck_timestamp'})
export class TimestampPipe implements PipeTransform {
    constructor(private datePipe: DatePipe) {}

    transform(value: any, pattern: string = 'mediumDate'): string {
        return value === 0 ? null : this.datePipe.transform(value, pattern);
    }
}
