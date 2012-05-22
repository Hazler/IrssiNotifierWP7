﻿using System;
using System.ComponentModel;
using System.IO.IsolatedStorage;
using System.Linq;
using System.Net;
using System.Windows;
using IrssiNotifier.Pages;
using Microsoft.Phone.Shell;
using Newtonsoft.Json.Linq;

namespace IrssiNotifier.Views
{
	public partial class RegisterView : INotifyPropertyChanged
	{
		public RegisterView(LoginPage page)
		{
			InitializeComponent();
			FromPage = page;

			DataContext = this;
			var webclient = new WebClient();
			webclient.UploadStringCompleted += (sender, args) =>
			{
				var result = args.Result;
				var parsed = JObject.Parse(result);	
				if (bool.Parse(parsed["success"].ToString()))
				{
					UserId = (string)parsed["userid"];
					FromPage.button.Content = "Jatka";
					FromPage.button.Visibility = Visibility.Visible;
					FromPage.button.Click += ButtonClick;
				}
				else
				{
					Dispatcher.BeginInvoke(() => MessageBox.Show("Virhe rekisteröinnissä: " + parsed["errorMessage"]));
					//TODO unsuccessful
				}
			};
			var cookies = PhoneApplicationService.Current.State["cookies"] as CookieCollection;
			var cookieHeader = cookies.Cast<Cookie>().Aggregate("", (current, cookie) => current + (cookie.Name + "=" + cookie.Value + "; "));
			webclient.Headers["Cookie"] = cookieHeader;
			webclient.Headers["Content-type"] = "application/x-www-form-urlencoded";
			webclient.UploadStringAsync(new Uri(App.Baseaddress + "client/register"), "guid=" + App.AppGuid + "&version=" + App.Version);
		}
		public LoginPage FromPage { get; private set; }

		private string _userId;
		public string UserId
		{
			get { return _userId; }
			set
			{
				_userId = value;
				IsolatedStorageSettings.ApplicationSettings["userID"] = value;
				if (PropertyChanged != null)
				{
					PropertyChanged(this, new PropertyChangedEventArgs("UserId"));
				}
			}
		}

		private void ButtonClick(object sender, EventArgs e)
		{
			PhoneApplicationService.Current.State["registered"] = true;
			FromPage.NavigationService.Navigate(new Uri("/Pages/MainPage.xaml", UriKind.Relative));
		}

		public event PropertyChangedEventHandler PropertyChanged;
	}
}
