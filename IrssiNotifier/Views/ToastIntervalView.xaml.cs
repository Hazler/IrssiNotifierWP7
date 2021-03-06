﻿using System.ComponentModel;
using System.Windows;
using IrssiNotifier.Interfaces;

namespace IrssiNotifier.Views
{
	public partial class ToastIntervalView : INotifyPropertyChanged
	{
		public ToastIntervalView()
		{
			InitializeComponent();
			DataContext = this;
			ToastInterval = SettingsView.GetInstance().ToastInterval;
		}

		private int _toastInterval;

		public int ToastInterval
		{
			get { return _toastInterval; }
			set
			{
				_toastInterval = value;
				NotifyPropertyChanged("ToastInterval");
			}
		}


		public void NotifyPropertyChanged(string property){
			if(PropertyChanged != null){
				PropertyChanged(this,new PropertyChangedEventArgs(property));
			}
		}

		public event PropertyChangedEventHandler PropertyChanged;

		private void OkButtonClick(object sender, RoutedEventArgs e)
		{
			SettingsView.GetInstance().ToastInterval = ToastInterval;
			var settingsPage = App.GetCurrentPage() as IViewContainerPage;
			if (settingsPage != null)
			{
				settingsPage.View = SettingsView.GetInstance();
			}
		}

		private void CancelButtonClick(object sender, RoutedEventArgs e)
		{
			var settingsPage = App.GetCurrentPage() as IViewContainerPage;
			if (settingsPage != null)
			{
				settingsPage.View = SettingsView.GetInstance();
			}
		}
	}
}
