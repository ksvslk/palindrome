//
//  UserGroupViewController.swift
//  PanModal
//
//  Created by Tosin Afolabi on 2/26/19.
//  Copyright © 2019 PanModal. All rights reserved.
//

import UIKit
import PanModal

class BaseMenuController: UITableViewController, PanModalPresentable {
    
    let padding = 16.0
    var isShortFormEnabled = false
 
    var menuItem: [MenuItemInfo] { return [] }

    override var preferredStatusBarStyle: UIStatusBarStyle {
        return .default
    }

    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupTableView()
        preferredContentSize = CGSize(width: self.view.bounds.width, height: getTotalHeight() + padding)
        popoverPresentationController?.containerView?.setNeedsLayout()
    }

    
    // MARK: - View Configurations
    func setupTableView() {
        tableView.separatorStyle = .singleLine
        tableView.backgroundColor = Theme.backgroundColor
        tableView.register(MenuItemCell.self, forCellReuseIdentifier: "cell")
        // Add an empty header view with desired height
        let headerView = UIView(frame: CGRect(x: 0, y: 0, width: tableView.bounds.width, height: padding))
        tableView.tableHeaderView = headerView
    }

    
    // MARK: - UITableViewDataSource
    override func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        let lastRowIndex = tableView.numberOfRows(inSection: indexPath.section) - 1

        if indexPath.row == lastRowIndex {
            // Hide the separator for the last row
            cell.separatorInset = UIEdgeInsets(top: 0, left: tableView.bounds.size.width, bottom: 0, right: 0)
        } else {
            // Show the separator for other rows
            cell.separatorInset = UIEdgeInsets.zero
        }
    }
    
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return menuItem.count
    }

    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as? MenuItemCell
            else { return UITableViewCell() }
        cell.configure(with: menuItem[indexPath.row])
        return cell
    }

    
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 60.0
    }
    
    
    var longFormHeight: PanModalHeight {
        return .contentHeight(getTotalHeight())
    }
    
    
    private func getTotalHeight() -> CGFloat
    {
        var totalHeight: CGFloat = 0.0
           for row in 0..<tableView.numberOfRows(inSection: 0) {
               let indexPath = IndexPath(row: row, section: 0)
               totalHeight += tableView(tableView, heightForRowAt: indexPath)
           }

           // Add any additional heights, such as the header view
           totalHeight += tableView.tableHeaderView?.frame.height ?? 0.0

        return totalHeight
    }

    
    // MARK: - UITableViewDelegate
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let actionButtonInfo = menuItem[indexPath.item]
        guard let cell = tableView.cellForRow(at: indexPath) as? MenuItemCell
            else { return  }
        actionButtonInfo.closure(cell)
    }

    
    // MARK: - Pan Modal Presentable
    var panScrollable: UIScrollView? {
        return tableView
    }

    
    var shortFormHeight: PanModalHeight {
        return isShortFormEnabled ? .contentHeight(300.0) : longFormHeight
    }
 

    var anchorModalToLongForm: Bool {
        return true
    }

    
    func willTransition(to state: PanModalPresentationController.PresentationState) {
        guard isShortFormEnabled, case .longForm = state
            else { return }

        isShortFormEnabled = false
        panModalSetNeedsLayoutUpdate()
    }

}
