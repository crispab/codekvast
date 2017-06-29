import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Params} from '@angular/router';

@Component({
    selector: 'ck-logged-out',
    template: require('./logged-out.component.html')
})
export class LoggedOutComponent implements OnInit {
    source: string;
    sourceApp: string;

    constructor(private route: ActivatedRoute) {
    }

    ngOnInit(): void {
        this.route.params
            .subscribe((params: Params) => {
                this.source = params['source'];
                this.sourceApp = params['sourceApp'];
            });
    }
}
