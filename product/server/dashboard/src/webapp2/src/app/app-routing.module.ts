import {CollectionStatusComponent} from './pages/collection-status/collection-status.component';
import {HomeComponent} from './pages/home/home.component';
import {IsLoggedIn} from './guards/is-logged-in.guard';
import {MethodDetailsComponent} from './pages/methods/method-details/method-details.component';
import {MethodsComponent} from './pages/methods/methods.component';
import {NgModule} from '@angular/core';
import {NotLoggedInComponent} from './pages/auth/not-logged-in/not-logged-in.component';
import {ReportGeneratorComponent} from './pages/report-generator/report-generator.component';
import {RouterModule, Routes} from '@angular/router';
import {VoteResultComponent} from './pages/vote-result/vote-result.component';

const routes: Routes = [
    {
        path: '',
        redirectTo: 'home',
        pathMatch: 'full'
    }, {
        path: 'home',
        component: HomeComponent
    }, {
        path: 'methods',
        component: MethodsComponent,
        canActivate: [IsLoggedIn]
    }, {
        path: 'method/:id',
        component: MethodDetailsComponent,
        canActivate: [IsLoggedIn]
    }, {
        path: 'not-logged-in',
        component: NotLoggedInComponent
    }, {
        path: 'status',
        component: CollectionStatusComponent,
        canActivate: [IsLoggedIn]
    }, {
        path: 'reports',
        component: ReportGeneratorComponent
    }, {
        path: 'vote-result/:feature/:vote',
        component: VoteResultComponent
    }, {
        path: '**',
        redirectTo: 'home',
        pathMatch: 'full'
    }
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}
