import {Component, Input, OnInit} from '@angular/core';
import {StateService} from '../services/state.service';
import {Router} from '@angular/router';
@Component({
    selector: 'ck-vote-for',
    template: require('./vote.component.html'),
})

class VoteState {
    vote: boolean = undefined;

    constructor(private router: Router, private feature: string) {
        console.log('Created a vote state for ' + feature);
    }

    doVote(choice: boolean) {
        console.log(`Voted ${choice} for ${this.feature}`);
        this.vote = choice;
        //noinspection JSIgnoredPromiseFromCall
        this.router.navigate(['/vote-result', this.feature, this.vote]);
    }

    hasVoted() {
        return this.vote !== undefined;
    }

    hasVotedFor(choice: boolean) {
        return this.vote === choice;
    }

    resetVote() {
        console.log(`Withdraw vote=${this.vote} for ${this.feature}`);
        //noinspection JSIgnoredPromiseFromCall
        this.router.navigate(['/vote-result', this.feature, 'withdraw-' + this.vote]);
        this.vote = undefined;
    }
}

@Component({
    selector: 'ck-vote-for',
    template: require('./vote.component.html'),
})
export class VoteComponent implements OnInit {
    state: VoteState;

    @Input('feature') featureName: string;

    constructor(private router: Router, private stateService: StateService) {
    }

    ngOnInit(): void {
        this.state = this.stateService.getState('vote_' + this.featureName, () => new VoteState(this.router, this.featureName));
    }
}
