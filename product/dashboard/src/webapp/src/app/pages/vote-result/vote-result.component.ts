import {ActivatedRoute, Params} from '@angular/router';
import {Component, OnInit} from '@angular/core';
import {Location} from '@angular/common';

@Component({
    selector: 'ck-vote-result',
    template: require('./vote-result.component.html'),
})
export class VoteResultComponent implements OnInit {
    featureName: string;
    vote: string;

    constructor(private location: Location, private route: ActivatedRoute) {
    }

    ngOnInit(): void {
        this.route.params
            .subscribe((params: Params) => {
                this.featureName = params['feature'];
                this.vote = params['vote'];
            });
    }

    goBack(): void {
        this.location.back();
    }

}
