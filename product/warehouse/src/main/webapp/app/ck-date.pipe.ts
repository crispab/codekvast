import {Pipe} from 'angular2/core';
import {DatePipe} from 'angular2/src/common/pipes/date_pipe';

@Pipe({name: 'ck_date'})
export class CkDatePipe extends DatePipe {

    transform(value: any, pattern?: string): string {
        return value === 0 ? null : super.transform(value, pattern);
    }
}
