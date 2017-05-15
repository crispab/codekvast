import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {HomeComponent} from './pages/home/home.component';
import {CollectionStatusComponent} from './pages/collection-status/collection-status.component';
import {MethodsComponent} from './pages/methods/methods.component';
import {MethodDetailComponent} from './pages/methods/method-detail.component';
import {ReportGeneratorComponent} from './pages/report-generator/report-generator.component';
import {VoteResultComponent} from './pages/vote-result/vote-result.component';
import {SsoComponent} from './components/sso.component';

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
        component: MethodsComponent
    }, {
        path: 'method/:id',
        component: MethodDetailComponent
    }, {
        path: 'sso/:token',
        component: SsoComponent
    }, {
        path: 'status',
        component: CollectionStatusComponent
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
